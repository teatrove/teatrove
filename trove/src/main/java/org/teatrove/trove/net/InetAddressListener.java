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

package org.teatrove.trove.net;

import java.net.*;

/**
 * A listener to be used in conjunction with {@link InetAddressResolver}.
 * The listener's methods are invoked when added into a InetAddressResolver,
 * and then only called again if anything has changed.
 *
 * @author Brian S O'Neill
 */
public interface InetAddressListener {
    public void unknown(UnknownHostException e);
    
    public void resolved(InetAddress[] addresses);
}
