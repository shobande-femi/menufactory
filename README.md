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
            password "8285e6e4bd0e4db7925923d8467fcf1024b144ec"
        }
    }
}

dependencies {
    compile("com.shobande:ussd-menu-builder:1.0.2")
}
```


#### Sample Usage
I use Ktor for serving requests.

Currently supported gateways are
* Africas Talking
* Hubtel
* Nsano

You can add support for any gateway by implementing the `Gateway` interface and passing
it as a param while instantiating the USSD Menu.

If you need help look through the source code to see how others were implemented.

You can use regular expression for matching inputs to next states.
You needn't escape any characters in the regex. Simply use triple quotes around the 
regex as is.

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

enum class States {
    CHECK_BALANCE,
    BUY_AIRTIME,
    CONTACT_US
}

fun fetchBalance(): Int = 100

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
```