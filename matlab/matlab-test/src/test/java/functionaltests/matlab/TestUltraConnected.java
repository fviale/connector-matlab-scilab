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
package functionaltests.matlab;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import java.io.File;

import org.junit.Ignore;
import org.ow2.proactive.scheduler.ext.common.util.IOTools;


/**
 * TestUltraConnected this test tests that a Scilab serie of jobs doesn't fail in case of scheduler restart. A scheduler
 * restart should be transparent to the scilab user
 *
 * @author The ProActive Team
 */
@Ignore // unstable
public class TestUltraConnected extends AbstractMatlabTest {

    static final int NB_ITER = 1;

    static final String TMPDIR = System.getProperty("java.io.tmpdir");

    @org.junit.Test
    public void run() throws Throwable {

        test_home = (new File(System.getProperty("pa.matsci.home") + fs + "classes" + fs + "matsciTests" + fs +
                              "functionaltests" + fs + "scilab")).getCanonicalFile();
        // Start the scheduler
        File proactiveConf = new File(test_home, "ProActiveConfiguration.xml");
        startCmdLine("pnp://localhost:9999", proactiveConf);

        runCommand(NB_ITER, "TestBasic");

    }

    protected void runCommand(int nb_iter, String functionName) throws Exception {

        ProcessBuilder pb = new ProcessBuilder();
        File proactiveConf = new File(test_home, "ProActiveConfiguration.xml");
        pb.directory(mat_tb_home);
        pb.redirectErrorStream(true);
        int runAsMe = 0;

        if (System.getProperty("proactive.test.runAsMe") != null) {
            runAsMe = 1;
        }
        if (System.getProperty("matlab.bin.path") != null) {
            pb.command(System.getProperty("matlab.bin.path"),
                       "-nodesktop",
                       "-nosplash",
                       "-r",
                       "addpath('" + test_home + "');RunUltraConnected('" + "pnp://localhost:9999" + "','" +
                             credFile.toString() + "','" + mat_tb_home + "'," + nb_iter + ",'" + functionName + "'," +
                             runAsMe + ");");
        } else {
            pb.command("matlab",
                       "-nodesktop",
                       "-nosplash",
                       "-r",
                       "addpath('" + test_home + "');RunUltraConnected('" + "pnp://localhost:9999" + "','" +
                             credFile.toString() + "','" + mat_tb_home + "'," + nb_iter + ",'" + functionName + "'," +
                             runAsMe + ");");
        }
        System.out.println("Running command : " + pb.command());

        File connectFile = new File(mat_tb_home + fs + "connect.tst");
        File okFile = new File(mat_tb_home + fs + "ok.tst");
        File koFile = new File(mat_tb_home + fs + "ko.tst");
        File reFile = new File(mat_tb_home + fs + "re.tst");

        if (connectFile.exists()) {
            connectFile.delete();
        }
        if (okFile.exists()) {
            okFile.delete();
        }

        if (koFile.exists()) {
            koFile.delete();
        }
        if (reFile.exists()) {
            reFile.delete();
        }

        Process p = pb.start();

        IOTools.LoggingThread lt1 = new IOTools.LoggingThread(p.getInputStream(), "[TestUltraConnected]", System.out);
        Thread t1 = new Thread(lt1, "TestUltraConnected");
        t1.setDaemon(true);
        t1.start();

        int code = -1;
        try {
            code = p.exitValue();
        } catch (Exception e) {

        }
        while (!connectFile.exists() && !koFile.exists() && !reFile.exists() && code == -1) {
            Thread.sleep(100);
            try {
                code = p.exitValue();
            } catch (Exception e) {
            }
        }
        if (connectFile.exists()) {
            System.out.println("Matlab session is connected.");
        }

        if (reFile.exists()) {
            // we restart in case of JIMS loading bug
            runCommand(nb_iter, functionName);
            return;
        }
        if (code != -1 || koFile.exists()) {
            assertFalse("Error during startup", code != -1 || koFile.exists());
        }
        // we are now connected to a scheduler, we will randomly restart the scheduler a nb_iter number of times

        for (int i = 0; i < nb_iter + 2; i++) {
            Thread.sleep((60 + Math.round(10 * Math.random())) * 1000);

            restartCmdLine("pnp://localhost:9999", proactiveConf);
        }

        p.waitFor();

        killScheduler();

        assertTrue(functionName + " passed", okFile.exists());

    }
}
