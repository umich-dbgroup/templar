package edu.umich.tbnalir.template;

/**
 * Created by cjbaik on 9/12/17.
 */
public class InstantiatedTemplate {
    String value;
    Double nlScore;
    Double templatePopularity;
    Double nlTemplateAffinity;

    public InstantiatedTemplate(String value) {
        this.value = value;
    }

    public Double getTotalScore() {
        // TODO: set weights here
        return Math.pow(getNlScore(), 2) + getTemplatePopularity() + getNlTemplateAffinity();
    }

    public Double getNlScore() {
        return nlScore;
    }

    public void setNlScore(Double nlScore) {
        this.nlScore = nlScore;
    }

    public Double getTemplatePopularity() {
        return templatePopularity;
    }

    public void setTemplatePopularity(Double templatePopularity) {
        this.templatePopularity = templatePopularity;
    }

    public Double getNlTemplateAffinity() {
        return nlTemplateAffinity;
    }

    public void setNlTemplateAffinity(Double nlTemplateAffinity) {
        this.nlTemplateAffinity = nlTemplateAffinity;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(this.getTotalScore());
        sb.append("] ");
        sb.append(this.value);
        return sb.toString();
    }

}
