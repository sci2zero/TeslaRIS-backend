package rs.teslaris.core.model.document;

public sealed interface PublisherPublishable
    permits Dataset, GeneticMaterial, MaterialProduct, Monograph, Patent, Proceedings, Software,
    Thesis {

    Publisher getPublisher();

    void setPublisher(Publisher publisher);
}
