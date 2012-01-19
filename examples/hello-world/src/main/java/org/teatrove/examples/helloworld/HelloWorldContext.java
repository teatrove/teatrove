package org.teatrove.examples.helloworld;

/**
 * This is a plain POJO context method that provides public getter methods.
 * Even though the {@link HelloWorldApplication} creates an instance of this
 * class as its context type to register within the TeaServlet, this class is
 * technically just a POJO service that has no dependency to Tea. The
 * application itself is responsible for the association and binding.
 */
public class HelloWorldContext {
	// the default greeting
	private String greeting;
	
	/**
	 * Main constructor to create an instance of this context with the given
	 * greeting.
	 * 
	 * @param greeting The greeting to use
	 */
	public HelloWorldContext(String greeting) {
		this.greeting = greeting;
	}
	
	/**
	 * Get the greeting for the given user based on the greeting defined for
	 * this instance (ie: provided in the constructor).
	 * 
	 * @param name The name of the user
	 * 
	 * @return The associated greeting for the user
	 */
	public String getGreeting(String name) {
		return this.greeting + ' ' + name;
	}
}