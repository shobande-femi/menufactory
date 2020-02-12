# Menufactory
A Kotlin DSL for wiring up your USSD app as a state machine.

Major credit to [Habbes](https://github.com/habbes/ussd-menu-builder) for
inspiring this project. 

This library is still in experimental state, however feel free to try it out.

Install by adding this to your `build.gradle`.

#### Gradle:
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
    compile("com.shobande:menufactory:1.0.0")
}
```
Find the repo password [here](https://drive.google.com/open?id=10yNyLXmrp5iwl8UFhi5crtRtdgLHOg7b)

## Sample Usage
```kotlin
suspend fun buildMenu(): Menu {
    return Menu.menu("Mystery App", HubTel) {
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
                "0" to this@menu.startStateName
                """^[0-9]*$""" to "airtimeBought"
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
                end("You have successfully bought some airtime")
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

### Instantiation
You can build your menu as shown above or create a singleton `Menu`, 
the subsequently adding states
##### Example
```kotlin
menu = Menu("Mystery App", HubTel)

menu.state("contactUs") {
    run {
        end("You can reach me on Twitter @shobande_femi")
    }
}
```

### Gateway
Currently supported gateways are
* Africas Talking
* Hubtel
* Nsano

You can add support for any gateway by implementing the `Gateway` interface and passing
it as a param while instantiating the USSD Menu.

In this example, I have chose HubTel as the gateway.

If you need help look through the source code to see how others were implemented.

### State Runner
The run method describes what this state displays to the user.
`con` responds to the user without ending the USSD session
`end` responds and ends the USSD session
Within the run method, you have access to the full request object.
To get the user's phone number simply do `it.phoneNumber`.
The request object contains 'phoneNumber', 'sessionId', 'serviceCode',
'message' and 'operator'.

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

Within a state's run, it possible to redirect to another state using `goTo`.
To redirect to the start state, you can use `goToStart`

If you need user display prettified, simply use triple quotes and
`trimMargin()` as seen in the example above.

### Transition Definitions
Transitions map the user input to the name of the next state.
The mapped value (next state) may be a string or a lambda that 
returns a string.
When using a lambda, the request object is accessible; `it.phoneNumber`.

**Note**: I have used enumerations for state names to avoid errors that may arise from
typos. When using enumerations, remember to retrieve the enum's `name`.

You can use regular expression for matching inputs to next states.
You needn't escape any characters in the regex. Simply use triple quotes around the 
regex as is.

When user input cannot be matched with any transition definition, the
state remains the same. You can also define a custom `defaultNextState`


## Serve Requests
Now we are ready to deploy our USSD app.
Feel free to use what ever mechanism to serve http requests.
For simplicity, I use Ktor in this example.
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