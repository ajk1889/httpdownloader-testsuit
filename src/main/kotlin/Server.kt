import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Server {
    private val ioScope = CoroutineScope(Dispatchers.IO)
    fun start() {
        ioScope.launch {

        }
    }
}