# Menufactory
Menufactory is a Kotlin DSL (Domain Specific Language) for building 
USSD applications as state machines.

With the outburst of USSD apps; especially in Africa, there still is 
little guidance about best practices for developing these apps.
On the surface, these apps are no different from regular web apps or
microservices exposed over REST. The tricky part becomes apparent with
handling navigations. This is the crux of USSD apps. 

**For every user input, the app must make a decision on the next
menu to display**

The annoyingly popular way of building these apps is using conditionals. 
Here's a snippet from Africa's talking documentation.
```php
...
if ($text == "1") {
    $response = "CON Choose account information you want to view \n";
    $response .= "1. Account number \n";
    $response .= "2. Account balance";
} else if ($text == "2") {
    $response = "END Your phone number is ".$phoneNumber;
}
...
echo $response;
```

Here are some issues with this approach:
* As the menu gets deeper, these conditionals can quickly grow into 
a gigantic mess
* We would have to write a conditional to match every possible user
input
* It isn't modular. One could have conditionals from menu screen 2
mixed up with conditionals from menu screen 10.

All in all, using these conditionals for handling navigation is
untenable. 

FSM according to Wikipedia;
##### It is an abstract machine that can be in exactly one of a finite number of states at any given time. The FSM can change from one state to another in response to some inputs; the change from one state to another is called a transition. An FSM is defined by a list of its states, its initial state, and the inputs that trigger each transition

Conceptually, we may think of every menu in the USSD app as a state in
the state machine. The first menu that is displayed when a user dials the
ussd code is the FSM's start state.
Each state is tagged with a unique identifier.
For any given input, the current state determines the next state of 
the FSM. The transitions employs the target state's unique identifier.
We use the sessionId to keep track of the FSM's current state.

In the past, I'd wired up some USSD apps as FSMs in a messy way.
While they were messy, they were more manageable than using gigantic
conditionals.

Then I came across [this](https://github.com/habbes/ussd-menu-builder) 
really nice JS library for building USSD apps as FSMs. 
Menufactory is highly inspired by that library.

This library is still in experimental state, however feel free to try it out.



#### Installation:
At the moment, it is hosted on Github Package Registry. Hence,
install by adding this to your `build.gradle`.

```groovy
repositories {
    maven {
        url "https://maven.pkg.github.com/shobande-femi/menufactory"
        credentials {
            username "shobande-femi"
            password "*********"
        }
    }
}

dependencies {
    compile("com.shobande:menufactory:1.1.0")
}
```
Find the repo password [here](https://drive.google.com/open?id=10yNyLXmrp5iwl8UFhi5crtRtdgLHOg7b)


### Instantiation
There are 2 ways to build start building your menu.

Create a singleton `Menu`, then register its states:
```kotlin
menu = Menu("Mystery App", HubTel, InMemorySession())

menu.startState {
    run {
        end("You can reach me on Twitter @shobande_femi")
    }
}
```
OR use the builder pattern:
```kotlin
Menu.menu("Mystery App", HubTel, InMemorySession()) {
    startState {
        run {
            end("You can reach me on Twitter @shobande_femi")
        }
    }
}
```

In the snippets above, we created a menu:
* called "Mystery App" 
* used Hubtel as our gateway of choice
* chose to store session data in memory
* registered the start state that displays some
text when visited.

### Gateway
A USSD gateway is a service that routes traffic from a USSD to your
app.

Currently supported gateways include:
* Africas Talking (Default)
* Hubtel
* Nsano

The major differences between these gateways message format they
pass to our application, and the response format they expect.
There are also slight naming differences. While one gateway may say
"phoneNumber", another may say "msisdn". 

You can add support for any gateway by implementing the `Gateway` 
interface and passing that object as a param while instantiating 
the USSD Menu.

The supported gateways implement that common interface. Looking through
their implementation may provide useful insight should you decide to
add support for another gateway.

### State Runner
The run method describes what this state displays to the user.
`con` responds to the user without ending the USSD session
`end` responds and ends the USSD session

These 2 methods accept strings. If you need user display prettified, 
simply use triple quotes and `trimMargin()` as seen in the example 
below.

```kotlin
Menu.menu("Mystery App") {
    startState {
        run {
            println(it.phoneNumber)

            con("""Welcome to ${this@menu.name}
                |1. Check Balance
                |2. Buy Airtime
                |3. Contact Us
            """.trimMargin())
        }
    }
}
```

Within the run method, you have access to the full request that comes
from the gateway.

Here's the full request object:
```kotlin
data class Request(
    val phoneNumber: String,
    val sessionId: String,
    val ussdCode: String?,
    val message: String,
    val operator: String?
)
```
To get the user's phone number simply do `it.phoneNumber`.

### Transition Definitions
Transitions map the user input to the name of the next state.
The mapped value (next state) may be a string or a lambda that 
returns a string.
When using a lambda, the request object is accessible; `it.phoneNumber`.

```kotlin
Menu.menu("Mystery App") {
    startState {
        run {
            con("""Welcome to ${this@menu.name}
                |1. Check Balance
                |2. Buy Airtime
                |3. Contact Us
            """.trimMargin())
        }
        transitions {
            "1" to "checkBalance"
            "2" to {
                if (!it.operator.equals("999999")) {
                    "buyAirtime"
                } else {
                    States.CONTACT_US.name
                }
            }
            "3" to States.CONTACT_US.name
            """^[a-zA-Z]*$""" to States.CONTACT_US.name
        }
        defaultNextState(States.CONTACT_US.name)
    }
}
```

**Note**: I have used enumerations for the contact us state names 
to avoid errors that may arise from typos. 
Generally, I prefer using enums to plain strings.
When using enumerations, remember to retrieve the enum's `name`.

You can use regular expression for matching inputs to next states.
You needn't escape any characters in the regex. Simply use triple quotes around the 
regex as is.

When user input cannot be matched with any transition definition, the
state doesn't change. 
You can also define a custom `defaultNextState`


### Adding more states
Only one start state can be registered. You can add as many other state
however.

When adding another state that isn't the start state, you must specify
a state name. This is the state's unique identifier.

```kotlin
Menu.menu("Mystery App") {
    startState {
        run {  }
        transitions {  }
    }
    state("checkBalance") {
        run {
            end("You balance is ${fetchBalance()}")
        }
        transitions {  }
    }
}
```

### State redirection
Within a state's run, it possible to redirect to another state using `goTo`.
To redirect to the start state, you can use `goToStart`.
Redirecting to another state doesn't break the state chain, hence, you
may redirect as many times as you wish.
Like redirecting to a state which redirects to another state and so on.

```kotlin
Menu.menu("Mystery App") {
    startState {
        run { goTo(States.CONTACT_US.name) }
    }
    state(States.CONTACT_US.name) {
        run {
            end("You can reach me on Twitter @shobande_femi")
        }
    }
}
```

### Sessions
A `Session` interface exists for defining interactions with whatever
storage you may use for saving session info. By default, the menu
used the `InMemorySession`. You may define yours by implementing
`Session`.

In your implementation, override `startStateName` and set to any value
of your choice. This will be a reserved state name and any state definition
(other than `startState`) that tries to use this name will throw 
a `ReservedStateName` exception.

The key methods to implement are;
* `start(sessionId: String)`: initializes a new session. Invoked internally
before any state is run. If there is an active session for the specified
sessionId, initialization is skipped
* `set(sessionId: String, key: String, value: Any)`: stores a key-value 
pair in the current user session
* `get(sessionId: String, key: String)`: retrieves a value from the current user
session
* `end(sessionId: String)`: deletes a user session. Invoked internally
by final states.

## Sample App
Checkout the full Sample app [here](https://github.com/shobande-femi/test-ussd-app)
```kotlin
suspend fun buildMenu(): Menu {
    return Menu.menu("Mystery App", HubTel, InMemorySession()) {
        startState {
            run {
                con("""Welcome to ${this@menu.name}
                    |1. Check Balance
                    |2. Buy Airtime
                    |3. Contact Us
                """.trimMargin())
            }

            transitions {
                "1" to States.CHECK_BALANCE.name
                "2" to {
                    if (!it.operator.equals("MTN")) {
                        States.BUY_AIRTIME.name
                    } else {
                        States.CONTACT_US.name
                    }
                }
                "3" to States.CONTACT_US.name
            }
        }

        state(States.CHECK_BALANCE.name) {
            run {
                if (it.phoneNumber.length != 14) {
                    goTo(States.CONTACT_US.name)
                } else {
                    end("You balance is ${fetchBalance()}")
                }
            }
        }

        state(States.BUY_AIRTIME.name) {
            run {
                con("""Enter your phone number
                    |0. Go back
                """.trimMargin())
            }
            transitions {
                "0" to session.startStateName
                """^[0-9]*$""" to {
                    session.set(it.sessionId, "phoneNumber", it.message)
                    "airtimeBought"
                }
                """^[a-zA-Z]*$""" to States.CONTACT_US.name
            }
            defaultNextState(States.CONTACT_US.name)
        }

        state(States.CONTACT_US.name) {
            run {
                end("You can reach me on Twitter @shobande_femi")
            }
        }

        state("airtimeBought") {
            run {
                end("You have successfully bought some airtime on ${session.get(it.sessionId, "phoneNumber")}")
            }
        }
    }
}

enum class States {
    CHECK_BALANCE,
    BUY_AIRTIME,
    CONTACT_US
}

suspend fun fetchBalance(): Int = 100
```

## Serve Requests
Now we are ready to deploy our USSD app.
Feel free to use what ever mechanism to serve http requests.
For simplicity, I use Ktor in this example.

Checkout the full Sample app [here](https://github.com/shobande-femi/test-ussd-app)

```kotlin
suspend fun main() {
    val menu = buildMenu()

    embeddedServer(Netty, 9090) {
        routing {
            install(ContentNegotiation) {
                gson {
                    setDateFormat(DateFormat.LONG)
                    setPrettyPrinting()
                }
            }

            post("/") {
                menu.handle(call.receive()) { call.respond(it) }
            }
        }
    }.start(wait = true)
}
```

The key part to note is `menu.handle`.
This method takes 2 arguments
1. The request body
2. A lambda to be called when the menu has processed the request and determined the 
result to be returned. That result is fed as an argument into the lambda.