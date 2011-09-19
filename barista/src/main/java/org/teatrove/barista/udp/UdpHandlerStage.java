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

package org.teatrove.barista.udp;

import org.teatrove.trove.log.Log;

/**
 * UdpHandlerStage is similar to a servlet filter, except it is specific to
 * {@link UdpHandler}. One key difference is that a handler stage may take
 * over the connection and process it asynchronously.
 *
 * @author Tammy Wang
 */
public interface UdpHandlerStage {
    /**
     * Similar to a servlet filter's doFilter method, except a boolean must be
     * returned. When true is returned, the caller should not close the
     * connection. The handler stage then assumes full responsibility for
     * closing the connection. A handler stage that detaches the connection in
     * this way can process it asynchronously, if it so chooses.
     * <p>
     * A handler stage is responsible for calling the next stage in the chain,
     * by calling the doNextStage method on the chain.
     *
     * @return true if caller should not close connection
     */
    public boolean handle(UdpServerConnection con, Chain chain)
        throws Exception;

    /**
     * Initializing with null implies that this stage is being removed from
     * service.
     */    
    public void init(Config config) throws Exception;

    public Config getConfig();

    public interface Chain {
        /**
         * @return true if caller should not close connection
         */
        public boolean doNextStage(UdpServerConnection con) throws Exception;
    }

    public interface Config extends org.teatrove.barista.util.Config {
        /**
         * Returns a log that emits {@link Impression} events after each
         * transaction is completed.
         */
        public Log getImpressionLog();
    }

    public class DefaultConfig extends org.teatrove.barista.util.ConfigWrapper
        implements Config
    {
        private Log mImpressionLog;

        public DefaultConfig(org.teatrove.barista.util.Config config
                             ) {
            super(config);
            
        }

        public Log getImpressionLog() {
            return mImpressionLog;
        }
    }
}
