package com.xxuz.piclane.smartpower.gql

import com.xxuz.piclane.smartpower.power.Instantaneous
import com.xxuz.piclane.smartpower.power.PowerObserver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.data.method.annotation.SubscriptionMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux

@Controller
class SubscriptionResolver(
    @Autowired
    val observer: PowerObserver
) {
    @SubscriptionMapping
    fun instantaneous() = Flux.from {
        observer.addPropertyChangeListener { event ->
            if(event.propertyName == PowerObserver.PROPERTY_INSTANTANEOUS) {
                it.onNext(event.newValue as Instantaneous)
            }
        }
    }
}
