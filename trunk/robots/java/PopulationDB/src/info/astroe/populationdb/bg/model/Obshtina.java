package info.astroe.populationdb.bg.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "obstina")
public class Obshtina {
    private int id;
    private String numeBg;
    private String numeRo;
    private int population;
    private Set<Settlement> settlements = new HashSet<Settlement>();
    private Map<Nationality, Integer> ethnicStructure = new HashMap<Nationality, Integer>();
    private Region region;

    @Id
    @GeneratedValue(generator = "increment")
    @GenericGenerator(name = "increment", strategy = "increment")
    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    @Column(name = "nume_bg")
    public String getNumeBg() {
        return numeBg;
    }

    public void setNumeBg(final String numeBg) {
        this.numeBg = numeBg;
    }

    @Column(name = "nume_ro")
    public String getNumeRo() {
        return numeRo;
    }

    public void setNumeRo(final String numeRo) {
        this.numeRo = numeRo;
    }

    @Column(name = "populatie")
    public int getPopulation() {
        return population;
    }

    public void setPopulation(final int population) {
        this.population = population;
    }

    @OneToMany(mappedBy = "obshtina")
    public Set<Settlement> getSettlements() {
        return settlements;
    }

    public void setSettlements(final Set<Settlement> settlements) {
        this.settlements = settlements;
    }

    @ElementCollection
    @CollectionTable(name = "obstina_nationalitate", joinColumns = @JoinColumn(name = "obstina"))
    @MapKeyJoinColumn(name = "nationalitate")
    @Column(name = "populatie")
    public Map<Nationality, Integer> getEthnicStructure() {
        return ethnicStructure;
    }

    public void setEthnicStructure(final Map<Nationality, Integer> ethnicStructure) {
        this.ethnicStructure = ethnicStructure;
    }

    @ManyToOne
    @JoinColumn(name = "regiune")
    public Region getRegion() {
        return region;
    }

    public void setRegion(final Region region) {
        this.region = region;
    }

}
