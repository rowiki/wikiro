package info.astroe.populationdb.bg.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "regiune")
public class Region {
    private int id;
    private String numeBg;
    private String numeRo;
    private Set<Obshtina> obshtinas = new HashSet<Obshtina>();

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

    @OneToMany(mappedBy = "region")
    public Set<Obshtina> getObshtinas() {
        return obshtinas;
    }

    public void setObshtinas(final Set<Obshtina> obshtinas) {
        this.obshtinas = obshtinas;
    }
}
