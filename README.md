# ussd-menu-builder
Build your USSD app as a state machine

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