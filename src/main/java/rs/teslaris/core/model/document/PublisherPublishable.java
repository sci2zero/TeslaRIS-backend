package rs.teslaris.core.model.document;

public sealed interface PublisherPublishable
    permits GeneticMaterial, MaterialProduct, Monograph, IntellectualProperty, Proceedings,
    IntangibleProduct,
    Thesis {

    Publisher getPublisher();

    void setPublisher(Publisher publisher);
}
