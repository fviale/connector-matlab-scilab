function PAjobResult(jobid)
    global ('PA_solver');
    PAensureConnected();
    if or(type(jobid)==[1 5 8]) then
        jobid = string(jobid);
    end
    try
        txt = jinvoke(PA_solver,'jobResult',jobid);
    catch
        PAensureConnected();
        txt = jinvoke(PA_solver,'jobResult',jobid);
    end
    disp(txt);
endfunction