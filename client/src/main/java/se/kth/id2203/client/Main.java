/*
 * The MIT License
 *
 * Copyright 2017 Lars Kroll <lkroll@kth.se>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package se.kth.id2203.client;

import org.apache.commons.cli.*;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.networking.NetAddressConverter;
import se.sics.kompics.Kompics;
import se.sics.kompics.config.Config;
import se.sics.kompics.config.ConfigUpdate;
import se.sics.kompics.config.Conversions;
import se.sics.kompics.config.ValueMerger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 *
 * Entry point of the Client Process/Component
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
public class Main {

    static final NetAddressConverter NAC = new NetAddressConverter();
    
    static {
        // conversions
        Conversions.register(NAC);
    }

    /**
     * Main method, read command line options: client port and bootstrap server ip:port, then spawn kompics runtime
     * and ParentComponent. Uses Apache Commons CLI library for parsing.
     * @param args
     */
    public static void main(String[] args) {
        Options opts = prepareOptions();
        HelpFormatter formatter = new HelpFormatter(); //Formatter for "help"-messages on the CLI
        CommandLine cmd;
        try {
            CommandLineParser cliparser = new DefaultParser();
            cmd = cliparser.parse(opts, args);
            // avoid constant conversion of the address by converting once and reassigning
            Config.Impl kompicsConfig = (Config.Impl) Kompics.getConfig();
            NetAddress self = kompicsConfig.getValue("id2203.project.address", NetAddress.class);
            Config.Builder configBuilder = kompicsConfig.modify(UUID.randomUUID());
            /**
             * Parse command-line options and update the kompics config
             */
            if (cmd.hasOption("p") || cmd.hasOption("i")) {
                String ip = self.asSocket().getHostString();
                int port = self.getPort();
                if (cmd.hasOption("p")) {
                    port = Integer.parseInt(cmd.getOptionValue("p"));
                }
                if (cmd.hasOption("i")) {
                    ip = cmd.getOptionValue("i");
                }
                self = new NetAddress(InetAddress.getByName(ip), port);
            }
            configBuilder.setValue("id2203.project.address", self);
            if (cmd.hasOption("b")) {
                String serverS = cmd.getOptionValue("b");
                NetAddress server = NAC.convert(serverS);
                if (server == null) {
                    System.err.println("Couldn't parse address string: " + serverS);
                    System.exit(1);
                }
                configBuilder.setValue("id2203.project.bootstrap-address", server);
            }
            ConfigUpdate configUpdate = configBuilder.finalise();
            kompicsConfig.apply(configUpdate, ValueMerger.NONE);
            /**
             * Start kompics runtime and ParentComponent
             */
            Kompics.createAndStart(ParentComponent.class);
        } catch (ParseException ex) {
            System.err.println("Invalid commandline options: " + ex.getMessage());
            formatter.printHelp("... <options>", opts);
            System.exit(1);
        } catch (UnknownHostException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }
    }

    /**
     * Returns Options object which represents a collection of Option Objects, which describe the possible options
     * for command-line. The client takes three options: local port and remote server ip:port.
     *
     * @return Options - main entry point into the cli-library
     */
    private static Options prepareOptions() {
        Options opts = new Options();

        opts.addOption("b", true, "Set Bootstrap server to <arg> (ip:port)");
        opts.addOption("p", true, "Changle local port to <arg> (default from config file)");
        opts.addOption("i", true, "Changle local ip to <arg> (default from config file)");
        return opts;
    }
}
