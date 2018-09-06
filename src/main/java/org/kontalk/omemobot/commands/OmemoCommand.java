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

package org.kontalk.omemobot.commands;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.omemo.OmemoFingerprint;
import org.jivesoftware.smackx.omemo.OmemoManager;
import org.jivesoftware.smackx.omemo.internal.CipherAndAuthTag;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.internal.OmemoMessageInformation;
import org.jivesoftware.smackx.omemo.listener.OmemoMessageListener;
import org.jxmpp.jid.impl.JidCreate;
import org.kontalk.konbot.client.XMPPTCPConnection;
import org.kontalk.konbot.shell.HelpableCommand;
import org.kontalk.konbot.shell.ShellSession;
import org.kontalk.konbot.shell.commands.AbstractCommand;
import org.kontalk.konbot.shell.commands.ConnectCommand;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;


@SuppressWarnings("unused")
public class OmemoCommand extends AbstractCommand implements HelpableCommand {

    private boolean initialized;
    private boolean supported;

    @Override
    public String name() {
        return "omemo";
    }

    @Override
    public String description() {
        return "OMEMO management";
    }

    @Override
    public void run(String[] args, ShellSession session) throws Exception {
        if (args.length < 2) {
            help();
            return;
        }

        XMPPTCPConnection conn = ConnectCommand.connection(session);
        if (conn == null || !conn.isConnected()) {
            println("Not connected.");
            return;
        }

        OmemoManager omemoManager = OmemoManager.getInstanceFor(conn);

        if (!initialized) {
            if (!OmemoManager.serverSupportsOmemo(conn, conn.getXMPPServiceDomain())) {
                println("Server does not support OMEMO.");
                supported = false;
                return;
            }
            supported = true;

            omemoManager.initialize();

            omemoManager.addOmemoMessageListener(new OmemoMessageListener() {
                @Override
                public void onOmemoMessageReceived(String decryptedBody, Message encryptedMessage, Message wrappingMessage, OmemoMessageInformation omemoInformation) {
                    println("onOmemoMessageReceived [decryptedBody=" + decryptedBody +
                            ", encryptedMessage=" + encryptedMessage +
                            ", wrappingMessage=" + wrappingMessage +
                            ", omemoInformation=" + omemoInformation +
                            "]");
                }

                @Override
                public void onOmemoKeyTransportReceived(CipherAndAuthTag cipherAndAuthTag, Message message, Message wrappingMessage, OmemoMessageInformation omemoInformation) {
                    println("onOmemoKeyTransportReceived [cipherAndAuthTag=" + cipherAndAuthTag +
                            ", message=" + message +
                            ", wrappingMessage=" + wrappingMessage +
                            ", omemoInformation=" + omemoInformation +
                            "]");
                }
            });
            initialized = true;
        }

        if (!supported) {
            println("Server does not support OMEMO.");
            return;
        }

        switch (args[1]) {
            case "send": {
                if (args.length < 4) {
                    println("Usage: " + name() + " send <jid> <body>");
                    return;
                }
                String jid = args[2];
                String body = args[3];
                Message message = omemoManager.encrypt(JidCreate.bareFromOrThrowUnchecked(jid), body);
                message.setTo(JidCreate.bareFromOrThrowUnchecked(jid));
                message.setType(Message.Type.chat);
                conn.sendStanza(message);
                break;
            }
            case "fingerprints": {
                if (args.length < 3) {
                    println("Usage: " + name() + " fingerprints <jid>");
                    return;
                }
                String jid = args[2];
                HashMap<OmemoDevice, OmemoFingerprint> fingerprints = omemoManager
                        .getActiveFingerprints(JidCreate.bareFromOrThrowUnchecked(jid));
                println("Active devices for " + jid);
                for (Map.Entry<OmemoDevice, OmemoFingerprint> f : fingerprints.entrySet()) {
                    println("deviceId: " + f.getKey().getDeviceId() + ", fingerprint: " + f.getValue().toString());
                }
                break;
            }
            case "trust": {
                if (args.length < 5) {
                    println("Usage: " + name() + " trust <jid> <device> <fingerprint>");
                    return;
                }
                String jid = args[2];
                int deviceId = Integer.parseInt(args[3]);
                String fingerprint = args[4];
                omemoManager.trustOmemoIdentity(new OmemoDevice(JidCreate.bareFromOrThrowUnchecked(jid), deviceId),
                        new OmemoFingerprint(fingerprint));
                break;
            }
            case "distrust": {
                if (args.length < 5) {
                    println("Usage: " + name() + " distrust <jid> <device> <fingerprint>");
                    return;
                }
                String jid = args[2];
                int deviceId = Integer.parseInt(args[3]);
                String fingerprint = args[4];
                omemoManager.distrustOmemoIdentity(new OmemoDevice(JidCreate.bareFromOrThrowUnchecked(jid), deviceId),
                        new OmemoFingerprint(fingerprint));
                break;
            }
            case "myfingerprint": {
                println(omemoManager.getOurFingerprint());
                break;
            }
        }

    }

    private String randomData(Properties dataset) {
        Random generator = new Random();
        // inefficient, but works
        Object[] values = dataset.values().toArray();
        return (String) values[generator.nextInt(values.length)];
    }

    public static Properties dataset(ShellSession session) {
        return (Properties) session.get("databot.dataset");
    }

    @Override
    public void help() {
        println("Usage: "+name()+" <command>");
    }
}
