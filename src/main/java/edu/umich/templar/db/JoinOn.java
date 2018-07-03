package edu.umich.templar.db;

import edu.umich.templar.db.el.Attribute;

public class JoinOn {
    private Attribute fk;
    private Attribute pk;

    public JoinOn(Attribute fk, Attribute pk) {
        this.fk = fk;
        this.pk = pk;
    }

    public Attribute getFk() {
        return fk;
    }

    public Attribute getPk() {
        return pk;
    }
}
