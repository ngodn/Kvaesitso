package de.mm20.launcher2.claudecli

import de.mm20.launcher2.search.SearchableRepository
import de.mm20.launcher2.search.Searchable
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val claudeCliModule = module {
    single<SearchableRepository<Searchable>>(named<ClaudeResult>()) { ClaudeCodeCLIRepository(get()) }
}
