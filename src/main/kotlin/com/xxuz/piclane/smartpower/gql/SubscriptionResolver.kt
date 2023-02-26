package com.xxuz.piclane.smartpower.gql

import com.xxuz.piclane.smartpower.model.Instantaneous
import com.xxuz.piclane.smartpower.power.PowerObserver
import org.reactivestreams.Subscription
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.data.method.annotation.SubscriptionMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import java.beans.PropertyChangeListener

@Controller
class SubscriptionResolver(
    @Autowired
    val observer: PowerObserver
) {
    @SubscriptionMapping
    fun instantaneous() = Flux.from { s ->
        val mutex = Object()
        var cancelled = false
        val listener = PropertyChangeListener { event ->
            if (event.propertyName == PowerObserver.PROPERTY_INSTANTANEOUS) {
                synchronized(mutex) {
                    if(cancelled) {
                        return@PropertyChangeListener
                    }
                    s.onNext(event.newValue as Instantaneous)
                }
            }
        }

        s.onSubscribe(object: Subscription {
            override fun cancel() {
                synchronized(mutex) {
                    observer.removePropertyChangeListener(listener)
                    cancelled = true
                }
            }
            override fun request(n: Long) {
                // nop
            }
        })

        observer.addPropertyChangeListener(listener)
    }
}
