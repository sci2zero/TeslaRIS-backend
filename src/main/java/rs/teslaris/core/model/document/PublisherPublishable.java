package rs.teslaris.core.model.document;

public sealed interface PublisherPublishable
    permits Proceedings, Dataset, Software, Patent, Thesis {

    void setPublisher(Publisher publisher);
}
