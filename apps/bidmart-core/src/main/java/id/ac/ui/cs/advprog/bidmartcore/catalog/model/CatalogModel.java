package id.ac.ui.cs.advprog.bidmartcore.catalog.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

// TODO: rename atau modif file template ini
@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name="catalog")
public class CatalogModel {
    @Id
    @GeneratedValue(strategy= GenerationType.UUID)
    private UUID id;

    @Column(name="SAMPLE_COLUMN", length=50)
    private String sampleColumn;
}
