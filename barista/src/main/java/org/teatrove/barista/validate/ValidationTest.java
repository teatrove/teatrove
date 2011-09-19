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

package org.teatrove.barista.validate;

import org.teatrove.trove.util.*;
import org.teatrove.trove.util.PropertyMapFactory;
import org.teatrove.barista.util.URLPropertyMapFactory;
import java.net.URL;

/**
 * @author Sean T. Treat
 */
public class ValidationTest {

    private static ValidationTest cValidator;

    public static void main(String[] args) {
        if(args.length < 1) {
            showUsage();
            return;
        }
        cValidator = new ValidationTest(args);
    }

    public static void showUsage() {
        System.out.println(
            "\nUsage: ValidationTest <properties-filename>");
    }

    public ValidationTest(String[] args) {
        // System.out.println("trying to load the system resource");
        // InputStream in = ClassLoader.getSystemClassLoader().
        //     getResourceAsStream("org/teatrove/validator/util/rules.properties");
    
        start(args);
        // show the results
    }

    protected void start(String[] args) {

        try {
            /*
            URL url = ClassLoader.getSystemClassLoader().
                getResource("org/teatrove/validator/util/rules.properties");
            // if (in == null) {
            if (url == null) {
                System.out.println("Failed to load the properties file");
            }
            else {
                System.out.println("succeeded in loading the props file: " + 
                                   url);
            }
            */

            PropertyMapFactory factory = createPropertyMapFactory(
                new String[] {args[0]});
            LineNumberCollector lnc = new LineNumberCollector();
            if (factory instanceof URLPropertyMapFactory) {
                ((URLPropertyMapFactory)factory).
                    setPropertyListener(lnc);
            }
            PropertyMap candidate = factory.createProperties();

            if (factory instanceof URLPropertyMapFactory) {
               ((URLPropertyMapFactory)factory).
                   setPropertyListener(null);
            }

            URL url = RulesEvaluator.getRulesURLForClass(
                "org/teatrove/validator/util/PropertyMapValidator");
            factory = createPropertyMapFactory(new String[] {url.toString()});
            PropertyMap validator = factory.createProperties();
            
            Validator pmv = 
                new BaristaPropertyMapValidator(candidate, validator, 
                                                lnc.getChangeMap());

            ConsoleErrorListener el = new ConsoleErrorListener();
            pmv.addValidationListener(el);
            pmv.validate();
            pmv.removeValidationListener(el);
            System.out.println();
            System.out.println(el.getErrorCount() + " errors found");
            System.out.println(el.getWarningCount() + " warnings found");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static PropertyMapFactory createPropertyMapFactory(String[] args)
        throws Exception
    {
        if (args.length < 1) {
            throw new Exception("Properties file required for first argument");
        }

        PropertyMapFactory factory;
        if (args.length == 1) {
            factory = new URLPropertyMapFactory(args[0]);
        }
        else {
            // Load and use custom PropertyMapFactory.
            Class factoryClass = Class.forName(args[1]);
            java.lang.reflect.Constructor ctor =
                factoryClass.getConstructor(new Class[]{String.class});
            factory =
                (PropertyMapFactory)ctor.newInstance(new Object[]{args[0]});
        }

        return factory;
    }
}

