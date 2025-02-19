package li.songe.gkd.service

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import li.songe.gkd.app
import li.songe.gkd.composition.CompositionExt.useLifeCycleLog
import li.songe.gkd.composition.CompositionExt.useScope
import li.songe.gkd.composition.CompositionService
import li.songe.gkd.notif.abNotif
import li.songe.gkd.notif.createNotif
import li.songe.gkd.notif.defaultChannel
import li.songe.gkd.util.allRulesFlow
import li.songe.gkd.util.clickCountFlow
import li.songe.gkd.util.map
import li.songe.gkd.util.storeFlow

class ManageService : CompositionService({
    useLifeCycleLog()
    val context = this
    createNotif(context, defaultChannel.id, abNotif)
    val scope = useScope()
    scope.launch {
        combine(
            allRulesFlow,
            clickCountFlow,
            storeFlow.map(scope) { it.enableService },
            GkdAbService.isRunning
        ) { allRules, clickCount, enableService, abRunning ->
            if (!abRunning) return@combine "无障碍未授权"
            if (!enableService) return@combine "服务已暂停"
            (if (allRules.allGroupSize > 0) {
                if (allRules.appSize > 0) {
                    "${allRules.appSize}应用/${allRules.allGroupSize}规则组"
                } else {
                    "${allRules.allGroupSize}规则组"
                }
            } else {
                "暂无规则"
            }) + if (clickCount > 0) "/${clickCount}点击" else ""
        }.stateIn(scope, SharingStarted.Eagerly, "").collect { text ->
            createNotif(
                context, defaultChannel.id, abNotif.copy(
                    text = text
                )
            )
        }
    }
    isRunning.value = true
    onDestroy {
        isRunning.value = false
    }
}) {
    companion object {
        fun start(context: Context = app) {
            context.startForegroundService(Intent(context, ManageService::class.java))
        }

        val isRunning = MutableStateFlow(false)

        fun stop(context: Context = app) {
            context.stopService(Intent(context, ManageService::class.java))
        }
    }
}