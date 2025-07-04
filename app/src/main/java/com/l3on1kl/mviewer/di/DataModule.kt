package com.l3on1kl.mviewer.di

import android.content.ContentResolver
import android.content.Context
import com.l3on1kl.mviewer.data.datasource.Local
import com.l3on1kl.mviewer.data.datasource.LocalMarkdownFileDataSource
import com.l3on1kl.mviewer.data.datasource.ReadableDocumentDataSource
import com.l3on1kl.mviewer.data.datasource.Remote
import com.l3on1kl.mviewer.data.datasource.RemoteMarkdownDataSource
import com.l3on1kl.mviewer.data.datasource.WritableDocumentDataSource
import com.l3on1kl.mviewer.data.repository.DocumentRepositoryImpl
import com.l3on1kl.mviewer.domain.repository.DocumentRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun contentResolver(@ApplicationContext ctx: Context): ContentResolver = ctx.contentResolver

    @Provides
    @Singleton
    @Local
    fun localDataSource(cr: ContentResolver): WritableDocumentDataSource =
        LocalMarkdownFileDataSource(cr)

    @Provides
    @Singleton
    @Remote
    fun remoteDataSource(): ReadableDocumentDataSource = RemoteMarkdownDataSource()

    @Provides
    @Singleton
    fun documentRepository(
        @Local local: WritableDocumentDataSource,
        @Remote remote: ReadableDocumentDataSource
    ): DocumentRepository = DocumentRepositoryImpl(local, remote)
}
