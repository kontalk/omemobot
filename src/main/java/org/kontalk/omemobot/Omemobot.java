/*
 * Omemobot
 * Copyright (C) 2018 Kontalk Devteam <devteam@kontalk.org>

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.kontalk.omemobot;

import org.apache.commons.lang3.ArrayUtils;
import org.jivesoftware.smackx.omemo.OmemoConfiguration;
import org.jivesoftware.smackx.omemo.signal.SignalOmemoService;
import org.kontalk.konbot.Konbot;
import org.kontalk.konbot.shell.BotShell;

import java.io.File;


public class Omemobot extends Konbot {

    private final String serverSpec;
    private final String personalKeyFile;
    private final String personalKeyPassphrase;

    public Omemobot(String[] args) {
        super(null);
        serverSpec = args[0];
        personalKeyFile = args[1];
        personalKeyPassphrase = args[2];
    }

    private void init() throws Exception {
        SignalOmemoService.acknowledgeLicense();
        SignalOmemoService.setup();
        OmemoConfiguration.setFileBasedOmemoStoreDefaultPath(new File("./keys"));
    }

    public void run() {
        try {
            BotShell sh = new BotShell();
            sh.setDebug(true);
            sh.init();
            sh.runCommand(ArrayUtils.addAll(new String[]{"server"}, serverSpec.split(" ")));
            sh.runCommand("personalkey", personalKeyFile, personalKeyPassphrase);
            sh.runCommand("connect");
            init();
            sh.start();
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: omemobot <server_spec> <personalkey_file> <personalkey_passphrase>");
            System.exit(1);
        }
        else {
            new Omemobot(args).run();
        }
    }

}
