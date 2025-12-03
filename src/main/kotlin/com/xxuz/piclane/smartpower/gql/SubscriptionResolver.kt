package com.xxuz.piclane.smartpower.gql

import com.xxuz.piclane.smartpower.model.Instantaneous
import com.xxuz.piclane.smartpower.power.PowerObserver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.data.method.annotation.SubscriptionMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import java.beans.PropertyChangeListener

@Controller
class SubscriptionResolver(
    @param:Autowired
    val observer: PowerObserver
) {
    @SubscriptionMapping
    fun instantaneous() = Flux.create { sink ->
        val listener = PropertyChangeListener { event ->
            if (event.propertyName == PowerObserver.PROPERTY_INSTANTANEOUS) {
                sink.next(event.newValue as Instantaneous)
            }
        }

        // サブスクリプションがキャンセルされた場合
        sink.onDispose {
            observer.removePropertyChangeListener(listener)
        }

        observer.addPropertyChangeListener(listener)
    }
}
