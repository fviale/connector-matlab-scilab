/*
 * ProActive Parallel Suite(TM):
 * The Open Source library for parallel and distributed
 * Workflows & Scheduling, Orchestration, Cloud Automation
 * and Big Data Analysis on Enterprise Grids & Clouds.
 *
 * Copyright (c) 2007 - 2017 ActiveEon
 * Contact: contact@activeeon.com
 *
 * This library is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation: version 3 of
 * the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 */
package functionaltests2;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.objectweb.proactive.utils.OperatingSystem;
import org.ow2.proactive.process_tree_killer.ProcessTree;
import org.ow2.proactive.resourcemanager.authentication.RMAuthentication;
import org.ow2.proactive.resourcemanager.frontend.RMConnection;
import org.ow2.proactive.scheduler.common.SchedulerConnection;
import org.ow2.proactive.scheduler.ext.common.util.IOTools;


/**
 * Starts Scheduler and Resource Manager.
 * It is used to start scheduler in a separate JVM than the Test itself.
 *
 * @author The ProActive Team
 */
public class SchedulerCommandLine implements Serializable {

    private static Process p;

    static final String fs = System.getProperty("file.separator");

    /**
     * Start a Scheduler and Resource Manager.
     */
    public static void startSchedulerCmdLine(boolean restart, File proactiveConf) throws Exception {

        File schedHome = new File(System.getProperty("pa.scheduler.home")).getCanonicalFile();
        File rmHome = new File(System.getProperty("pa.rm.home")).getCanonicalFile();
        if (proactiveConf != null) {
            FileUtils.copyFile(proactiveConf,
                               new File(schedHome, "config" + fs + "proactive" + fs + "ProActiveConfiguration.xml"));
        }

        System.out.println(schedHome);

        p = null;
        ProcessBuilder pb = new ProcessBuilder();
        if (OperatingSystem.getOperatingSystem().equals(OperatingSystem.unix)) {
            pb.directory(new File(schedHome + fs + "bin" + fs + "unix"));
            pb.command("/bin/bash",
                       restart ? "scheduler-start" : "scheduler-start-clean",
                       "-Dproactive.communication.protocol=pnp",
                       "-Dproactive.pnp.port=9999");
            pb.environment().put("SchedulerTStarter", "SchedulerTStarter");
            p = pb.start();

        } else {

            pb.directory(new File(schedHome + fs + "bin" + fs + "windows"));

            pb.command("cmd.exe",
                       "/c",
                       restart ? "scheduler-start.bat" : "scheduler-start-clean.bat",
                       "-Dproactive.communication.protocol=pnp",
                       "-Dproactive.pnp.port=9999");
            pb.environment().put("SchedulerTStarter", "SchedulerTStarter");
            p = pb.start();

        }

        IOTools.LoggingThread lt1 = new IOTools.LoggingThread(p.getInputStream(), "[SchedulerTStarter]", System.out);
        Thread t1 = new Thread(lt1, "SchedulerTStarter");
        t1.setDaemon(true);
        t1.start();

        // waiting the initialization
        RMAuthentication rmAuth = RMConnection.waitAndJoin("pnp://localhost:9999");

        System.out.println("RM successfully joined.");

        SchedulerConnection.waitAndJoin("pnp://localhost:9999");
        System.out.println("Scheduler successfully joined.");

    }

    public static void killSchedulerCmdLine() {
        HashMap<String, String> env = new HashMap<String, String>();
        env.put("SchedulerTStarter", "SchedulerTStarter");

        // and eventually we kill remaining nodes
        try {
            ProcessTree.get().killAll(p, env);
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //p.destroy();
    }
}
