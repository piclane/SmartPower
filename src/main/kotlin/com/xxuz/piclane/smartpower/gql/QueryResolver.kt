package com.xxuz.piclane.smartpower.gql

import com.xxuz.piclane.smartpower.model.Instantaneous
import com.xxuz.piclane.smartpower.model.PowerSource
import com.xxuz.piclane.smartpower.power.PowerObserver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class QueryResolver(
    @Autowired
    private val observer: PowerObserver,

    @Autowired
    private val powerSource: PowerSource
) {
    @QueryMapping
    fun instantaneous(): Instantaneous = observer.getInstantaneous()

    @QueryMapping
    fun powerSource() = powerSource
}
