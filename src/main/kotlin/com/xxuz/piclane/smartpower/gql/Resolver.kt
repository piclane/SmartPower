package com.xxuz.piclane.smartpower.gql

import com.xxuz.piclane.smartpower.power.PowerObserver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class Resolver(
    @Autowired
    val observer: PowerObserver
) {
    @QueryMapping
    fun instantaneousPower() = observer.getInstantaneousPower()

    @QueryMapping
    fun instantaneousCurrent() = observer.getInstantaneousCurrent()
}
