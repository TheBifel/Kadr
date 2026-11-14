package com.thebifel.kadr.di

import android.content.Context
import com.thebifel.kadr.KadrApp
import com.thebifel.kadr.data.content.AssetContent
import com.thebifel.kadr.data.local.PreferencesWidgetRepository
import com.thebifel.kadr.data.rendering.WebViewFrameRenderer
import com.thebifel.kadr.domain.port.*
import com.thebifel.kadr.domain.usecase.*
import com.thebifel.kadr.platform.AndroidEnvironment
import com.thebifel.kadr.platform.widget.AndroidWidgetHost
import com.thebifel.kadr.platform.work.WorkManagerRefreshScheduler

/** The single composition root. Android entry points never construct their own dependency graph. */
class AppContainer(context: Context) {
    private val context = context.applicationContext
    val repository: WidgetRepository = PreferencesWidgetRepository(this.context)
    val content = AssetContent(this.context)
    val environment: EnvironmentProvider = AndroidEnvironment(this.context)
    val renderer: FrameRenderer = WebViewFrameRenderer(this.context)
    val widgets: WidgetHost = AndroidWidgetHost(this.context, repository)
    val scheduler: RefreshScheduler = WorkManagerRefreshScheduler(this.context)
    val requestRefresh = RequestRefresh(repository, scheduler, widgets)
    val saveProject = SaveProject(repository, scheduler, requestRefresh)
    val deleteProject = DeleteProject(repository, scheduler, widgets)
    val bindWidget = BindWidget(repository, scheduler, requestRefresh)
    val reconcile = ReconcileWidgets(repository, scheduler, widgets, requestRefresh)
    val refreshProject = RefreshProject(repository, renderer, environment, widgets)
}

val Context.appContainer: AppContainer
    get() = (applicationContext as KadrApp).container
