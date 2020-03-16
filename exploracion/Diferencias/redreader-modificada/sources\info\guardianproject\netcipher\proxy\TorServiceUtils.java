package info.guardianproject.netcipher.proxy;

import android.content.Context;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;
import java.util.StringTokenizer;
import org.apache.commons.lang3.StringUtils;

public class TorServiceUtils {
    public static final String CHMOD_EXE_VALUE = "700";
    public static final String SHELL_CMD_CHMOD = "chmod";
    public static final String SHELL_CMD_KILL = "kill -9";
    public static final String SHELL_CMD_PIDOF = "pidof";
    public static final String SHELL_CMD_PS = "ps";
    public static final String SHELL_CMD_RM = "rm";
    private static final String TAG = "TorUtils";

    public static boolean isRootPossible() {
        StringBuilder log = new StringBuilder();
        try {
            if (new File("/system/app/Superuser.apk").exists() || new File("/system/app/superuser.apk").exists()) {
                return true;
            }
            if (new File("/system/bin/su").exists()) {
                if (doShellCommand(new String[]{"su"}, log, false, true) != 0) {
                    return false;
                }
                return true;
            }
            if (doShellCommand(new String[]{"which su"}, log, false, true) == 0) {
                Log.d(TAG, "root exists, but not sure about permissions");
                return true;
            }
            Log.e(TAG, "Could not acquire root permissions");
            return false;
        } catch (IOException e) {
            Log.e(TAG, "Error checking for root access", e);
        } catch (Exception e2) {
            Log.e(TAG, "Error checking for root access", e2);
        }
    }

    public static int findProcessId(Context context) {
        String dataPath = context.getFilesDir().getParentFile().getParentFile().getAbsolutePath();
        StringBuilder sb = new StringBuilder();
        sb.append(dataPath);
        sb.append("/");
        sb.append(OrbotHelper.ORBOT_PACKAGE_NAME);
        sb.append("/app_bin/tor");
        String command = sb.toString();
        try {
            int procId = findProcessIdWithPidOf(command);
            if (procId == -1) {
                return findProcessIdWithPS(command);
            }
            return procId;
        } catch (Exception e) {
            try {
                return findProcessIdWithPS(command);
            } catch (Exception e2) {
                String str = TAG;
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Unable to get proc id for command: ");
                sb2.append(URLEncoder.encode(command));
                Log.e(str, sb2.toString(), e2);
                return -1;
            }
        }
    }

    public static int findProcessIdWithPidOf(String command) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec(new String[]{SHELL_CMD_PIDOF, new File(command).getName()}).getInputStream()));
        while (true) {
            String readLine = reader.readLine();
            String line = readLine;
            if (readLine == null) {
                return -1;
            }
            try {
                return Integer.parseInt(line.trim());
            } catch (NumberFormatException e) {
                StringBuilder sb = new StringBuilder();
                sb.append("unable to parse process pid: ");
                sb.append(line);
                Log.e("TorServiceUtils", sb.toString(), e);
            }
        }
    }

    public static int findProcessIdWithPS(String command) throws Exception {
        String line;
        StringBuilder sb;
        BufferedReader reader = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec(SHELL_CMD_PS).getInputStream()));
        do {
            String readLine = reader.readLine();
            line = readLine;
            if (readLine == null) {
                return -1;
            }
            sb = new StringBuilder();
            sb.append(' ');
            sb.append(command);
        } while (line.indexOf(sb.toString()) == -1);
        StringTokenizer st = new StringTokenizer(line, StringUtils.SPACE);
        st.nextToken();
        return Integer.parseInt(st.nextToken().trim());
    }

    public static int doShellCommand(String[] cmds, StringBuilder log, boolean runAsRoot, boolean waitFor) throws Exception {
        Process proc;
        if (runAsRoot) {
            proc = Runtime.getRuntime().exec("su");
        } else {
            proc = Runtime.getRuntime().exec("sh");
        }
        OutputStreamWriter out = new OutputStreamWriter(proc.getOutputStream());
        for (String write : cmds) {
            out.write(write);
            out.write(StringUtils.LF);
        }
        out.flush();
        out.write("exit\n");
        out.flush();
        if (!waitFor) {
            return -1;
        }
        char[] buf = new char[10];
        InputStreamReader reader = new InputStreamReader(proc.getInputStream());
        while (true) {
            int read = reader.read(buf);
            int read2 = read;
            if (read == -1) {
                break;
            } else if (log != null) {
                log.append(buf, 0, read2);
            }
        }
        InputStreamReader reader2 = new InputStreamReader(proc.getErrorStream());
        while (true) {
            int read3 = reader2.read(buf);
            int read4 = read3;
            if (read3 == -1) {
                return proc.waitFor();
            }
            if (log != null) {
                log.append(buf, 0, read4);
            }
        }
    }
}
