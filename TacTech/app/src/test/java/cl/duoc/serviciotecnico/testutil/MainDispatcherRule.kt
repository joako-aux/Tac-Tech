package cl.duoc.serviciotecnico.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Regla de JUnit para probar ViewModels.
 *
 * viewModelScope usa Dispatchers.Main, que en la JVM no existe (es el hilo de la UI de Android).
 * Esta regla lo reemplaza por un dispatcher de prueba antes de cada test y lo restaura despues.
 * Con UnconfinedTestDispatcher las corrutinas se ejecutan de inmediato: al volver de
 * vm.guardar() el resultado ya esta en el estado.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
