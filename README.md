# ussd-menu-builder
Build your USSD app as a state machine

This library is still in experimental state, however feel free to try it out.

Install by adding this to your `build.gradle`.

##### Gradle:
```groovy
repositories {
    maven {
        url "https://maven.pkg.github.com/shobande-femi/ussd-menu-builder"
        credentials {
            username "shobande-femi"
            password "*********"
        }
    }
}

dependencies {
    compile("com.shobande:ussd-menu-builder:1.0.2")
}
```
Hit me up, and I'll send you the password.


#### Sample Usage
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
                "1" to_ States.CHECK_BALANCE
                "2" to_ {
                    if (fetchBalance() <= Int.MAX_VALUE) {
                        States.BUY_AIRTIME
                    } else {
                        States.CONTACT_US
                    }
                }
                "3" to_ States.CONTACT_US
                """^[a-zA-Z]*$""" to_ States.CONTACT_US
            }
        }

        state(States.CHECK_BALANCE) {
            run {
                end("You balance is ${fetchBalance()}")
            }
        }

        state(States.BUY_AIRTIME) {
            run {
                con("Enter your phone number")
            }
            transitions {
                """^[0-9]*$""" to "selectNetworkProvider"
            }
            defaultNextState(startStateName)
        }

        state(States.CONTACT_US) {
            run {
                end("You can reach me on Twitter @shobande_femi")
            }
        }
    }
}

enum class States {
    CHECK_BALANCE,
    BUY_AIRTIME,
    CONTACT_US
}

fun fetchBalance(): Int = 100
```

##### Instantiation
You can build your menu as shown above or create a singleton `Menu`, 
the subsequently adding states
###### Example
```kotlin
menu = Menu("Mystery App", HubTel)

menu.state("contactUs") {
    run {
        end("You can reach me on Twitter @shobande_femi")
    }
}
```

##### Gateway
Currently supported gateways are
* Africas Talking
* Hubtel
* Nsano

You can add support for any gateway by implementing the `Gateway` interface and passing
it as a param while instantiating the USSD Menu.

In this example, I have chose HubTel as the gateway.

If you need help look through the source code to see how others were implemented.

##### State Runner
The run method describes what this state displays to the user.
`con` responds to the user without ending the USSD session
`end` responds and ends the USSD session
Within the run method, you have access to the full request object.
To get the user's phone number simply do `it.phoneNumber`.
The request object contains 'phoneNumber', 'sessionId', 'serviceCode',
'message' and 'operator'.

If you need user display prettified, simply use triple quotes and
`trimMargin()` as seen in the example above.

##### Transition Definitions
Transitions map the user input to the name of the next state.
The name of the next state can be a plain old string, an enumeration or
a lambda that returns a string/enumeration. I suggest always using
enumeration as they are less prone to errors.

When you map to an enumeration, use `to_` (with an underscore).
The reason for this is documented [here](https://github.com/shobande-femi/ussd-menu-builder/blob/42f375e6963ca449a123e21a33915c78abeeec26/src/main/kotlin/state/StateHandler.kt#L50)

You can use regular expression for matching inputs to next states.
You needn't escape any characters in the regex. Simply use triple quotes around the 
regex as is.

When user input cannot be matched with any transition definition, the
state remains the same. You can also define a custom `defaultNextState`


#### Serve Requests
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

            post("/ussd") {
                menu.run(call.receive()) { call.respond(it) }
            }
        }
    }.start(wait = true)
}
```