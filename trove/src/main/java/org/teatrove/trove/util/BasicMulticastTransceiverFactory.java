/*
 *  Copyright 1997-2011 teatrove.org
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.teatrove.trove.util;

import java.util.Properties;
import java.net.InetAddress;

/**
 * Factory for basic multicast implementation.
 *
 * @author: Guy Molinari
 */
public class BasicMulticastTransceiverFactory implements MessageTransceiverFactory {

    public MessageTransceiver create(Properties props) throws MessageException {

        int port = 0;
        try {
            port = Integer.parseInt(props.getProperty("port"));
        }
        catch (NumberFormatException ignore) {}
     
        InetAddress group = null;
        InetAddress bindAddress = null;
        try {
            group = InetAddress.getByName("group");
            bindAddress = InetAddress.getByName("bindAddress");
        }
        catch (Exception e) {
            throw new MessageException("Cannot create transeiver instance ", e);
        }
        return new BasicMulticastTransceiverImpl(group, port, bindAddress);

    }

}
