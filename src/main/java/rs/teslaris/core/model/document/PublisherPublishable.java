package rs.teslaris.core.model.document;

public sealed interface PublisherPublishable
    permits Proceedings, Dataset, Software, Patent, Thesis, Monograph {

    Publisher getPublisher();

    void setPublisher(Publisher publisher);
}
