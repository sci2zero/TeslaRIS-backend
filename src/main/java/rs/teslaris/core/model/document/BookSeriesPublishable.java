package rs.teslaris.core.model.document;

public sealed interface BookSeriesPublishable permits Proceedings, Monograph {

    void setPublicationSeries(PublicationSeries publicationSeries);
}
