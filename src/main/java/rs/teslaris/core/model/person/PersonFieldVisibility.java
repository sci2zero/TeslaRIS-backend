package rs.teslaris.core.model.person;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.commontypes.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DynamicUpdate
@Table(name = "person_public_field_visibility", indexes = {
    @Index(name = "idx_person_field_visibility_person_id", columnList = "person_id")
})
@SQLRestriction("deleted=false")
public class PersonFieldVisibility extends BaseEntity {

    @Column(name = "phone_number_visible")
    private Boolean phoneNumberVisible = false;

    @Column(name = "contact_email_visible")
    private Boolean contactEmailVisible = false;

    @Column(name = "date_of_birth_visible")
    private Boolean dateOfBirthVisible = false;

    @Column(name = "sex_visible")
    private Boolean sexVisible = false;

    @Column(name = "birthplace_visible")
    private Boolean birthplaceVisible = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", referencedColumnName = "id", nullable = false)
    private Person person;
}
