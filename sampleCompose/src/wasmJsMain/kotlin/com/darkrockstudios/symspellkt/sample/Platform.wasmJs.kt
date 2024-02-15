package com.darkrockstudios.symspellkt.sample

import kotlinx.browser.window
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()

@OptIn(ExperimentalContracts::class)

actual fun measureMillsTime(block: () -> Unit): Double {
	contract {
		callsInPlace(block, InvocationKind.EXACTLY_ONCE)
	}
	val start = window.performance.now()
	block()
	val end = window.performance.now()
	return (end - start)
}