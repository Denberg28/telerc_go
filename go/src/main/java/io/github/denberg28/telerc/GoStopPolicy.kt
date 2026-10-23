package io.github.denberg28.telerc

/** Torque-off is an explicit action on a balancer, not a link-loss policy. */
internal object GoStopPolicy {
    fun disarm(balancing: Boolean, explicitOperatorDisarm: Boolean) =
        explicitOperatorDisarm || !balancing
    fun armReplyPending(result: Int) = result == 0 || result == 5
}
