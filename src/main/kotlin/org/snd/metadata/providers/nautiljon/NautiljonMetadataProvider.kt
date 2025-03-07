package org.snd.metadata.providers.nautiljon

import org.snd.metadata.MetadataProvider
import org.snd.metadata.NameSimilarityMatcher
import org.snd.metadata.model.Provider
import org.snd.metadata.model.Provider.NAUTILJON
import org.snd.metadata.model.SeriesSearchResult
import org.snd.metadata.model.metadata.ProviderBookId
import org.snd.metadata.model.metadata.ProviderBookMetadata
import org.snd.metadata.model.metadata.ProviderSeriesId
import org.snd.metadata.model.metadata.ProviderSeriesMetadata
import org.snd.metadata.providers.nautiljon.model.SeriesId
import org.snd.metadata.providers.nautiljon.model.VolumeId
import org.snd.metadata.providers.nautiljon.model.toSeriesSearchResult

class NautiljonMetadataProvider(
    private val client: NautiljonClient,
    private val metadataMapper: NautiljonSeriesMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
    private val fetchSeriesCovers: Boolean,
    private val fetchBookCovers: Boolean,
) : MetadataProvider {

    override fun providerName(): Provider {
        return NAUTILJON
    }

    override fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val series = client.getSeries(SeriesId(seriesId.id))
        val thumbnail = if (fetchSeriesCovers) client.getSeriesThumbnail(series) else null

        return metadataMapper.toSeriesMetadata(series, thumbnail)
    }

    override fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        val bookMetadata = client.getBook(SeriesId(seriesId.id), VolumeId(bookId.id))
        val thumbnail = if (fetchBookCovers) client.getVolumeThumbnail(bookMetadata) else null

        return metadataMapper.toBookMetadata(bookMetadata, thumbnail)
    }

    override fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val searchResults = client.searchSeries(seriesName.take(400)).take(limit)
        return searchResults.map { it.toSeriesSearchResult() }
    }

    override fun matchSeriesMetadata(seriesName: String): ProviderSeriesMetadata? {
        val searchResults = client.searchSeries(seriesName.take(400))
        val match = searchResults
            .firstOrNull { nameMatcher.matches(seriesName, listOfNotNull(it.title, it.alternativeTitle)) }

        return match?.let {
            val series = client.getSeries(it.id)
            val thumbnail = if (fetchSeriesCovers) client.getSeriesThumbnail(series) else null
            metadataMapper.toSeriesMetadata(series, thumbnail)
        }
    }
}
