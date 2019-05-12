// Copyright (C) 2019 Dhipo Alam <dhipo.alam@outlook.com>
// 
// This file is part of AndroidGUI.
// 
// AndroidGUI is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
// 
// AndroidGUI is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with AndroidGUI.  If not, see <http://www.gnu.org/licenses/>.

package id.dhipo.sdkbridge;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AndroidSDK {
    private File sdkPath;
    private Consumer<SDKAction> listener;

    public static void main(String[] args) {
        AndroidSDK sdk = new AndroidSDK(System.getenv("ANDROID_HOME"));
        sdk.setListener((action) -> {
            switch (action.getAction()) {
            case SDKAction.ACTION_PROGRESS:
                System.out.println(action.getPayload().concat("%"));
                break;
            case SDKAction.ACTION_STATUS:
                System.out.println(action.getPayload());
                break;
            case SDKAction.ACTION_AVAILABLE:
                System.out.println(String.join("", action.getPayload(), " v", action.getExtra()[0]));
                break;
            default:
                break;
            }
        });
        sdk.getPackageList();
    }

    public AndroidSDK(String sdkPath) {
        this.sdkPath = new File(sdkPath);
    }

    public AndroidSDK(File sdkPath, Consumer<SDKAction> listener) {
        this.sdkPath = sdkPath;
        this.listener = listener;
    }

    public AndroidSDK(String sdkPath, Consumer<SDKAction> listener) {
        this.sdkPath = new File(sdkPath);
        this.listener = listener;
    }

    public AndroidSDK(File sdkPath) {
        this.sdkPath = sdkPath;
    }

    public void setListener(Consumer<SDKAction> listener) {
        this.listener = listener;
    }

    private void procSdkManager(String param) {
        try {
            // enter code here
            String osname = System.getProperty("os.name");
            String sdkmanagerCmd = "sdkmanager";
            if (osname.contains("Windows")){
                sdkmanagerCmd = "sdkmanager.bat";
            }
            Process proc = Runtime.getRuntime().exec(Paths
                    .get(sdkPath.getAbsolutePath(), "tools", "bin", String.join(" ", sdkmanagerCmd, param)).toString());

            // enter code here

            try (BufferedReader input = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                String status = SDKAction.ACTION_OTHER;
                while ((line = input.readLine()) != null) {
                    //System.out.println(line);
                    // Parse progress status
                    if (line.contains("[")) {
                        Pattern reProgress = Pattern.compile("([0-9]+)[\\s%]+([A-Za-z]+.*)");
                        Matcher mtProgress = reProgress.matcher(line);
                        if (mtProgress.find()) {
                            listener.accept(new SDKAction(SDKAction.ACTION_PROGRESS, mtProgress.group(1)));
                            listener.accept(new SDKAction(SDKAction.ACTION_STATUS, mtProgress.group(2)));
                        }
                    } else if (line.contains("Installed")) {
                        status = SDKAction.ACTION_INSTALLED;
                    } else if (line.contains("Available")) {
                        status = SDKAction.ACTION_AVAILABLE;
                    } else if (line.contains("|") && !line.contains("Path") && !line.contains("---")) {
                        Pattern rePackages;
                        if (status == SDKAction.ACTION_AVAILABLE){
                            rePackages = Pattern.compile("(.*)\\s+\\|(.*)\\s+\\|(.*)\\s*");
                        } else {
                            rePackages = Pattern.compile("(.*)\\s+\\|(.*)\\s+\\|(.*)\\s+\\|(.*)");
                        }
                        Matcher mtPackages = rePackages.matcher(line);
                        if (mtPackages.find()) {
                            SDKAction action = new SDKAction(status, mtPackages.group(1).trim());
                            String[] extra = { mtPackages.group(2).trim(), mtPackages.group(3).trim() };
                            action.setExtra(extra);
                            listener.accept(action);
                        }
                    } else {
                        listener.accept(new SDKAction(SDKAction.ACTION_OTHER, line));
                    }
                }
            }

        } catch (Exception err) {
            listener.accept(new SDKAction(SDKAction.ACTION_ERROR, err.getMessage()));
        } finally {
            listener.accept(new SDKAction(SDKAction.ACTION_DONE, ""));
        }

    }

    public void getPackageList() {
        procSdkManager("--list");
    }

    public void updateRepo(){
        procSdkManager("--update");
    }

    public void installPackage(String packageName){
        procSdkManager(packageName);
    }

    public void installPackages(String[] packageNames){
        String param = String.join(" ", packageNames);
        procSdkManager(param);
    }

    public void removePackages(String[] packageNames){
        String param = "--uninstall " + String.join(" ", packageNames);
        procSdkManager(param);
    }
}