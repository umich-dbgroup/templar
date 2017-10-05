package edu.umich.templar.template;

/**
 * Created by cjbaik on 9/12/17.
 */
public class InstantiatedTemplate {
    Template template;
    String value;
    Double nlScore;
    Double templatePopularity;
    Double complexity;
    Double nlTemplateAffinity;

    public InstantiatedTemplate(String value) {
        this.value = value;
    }

    public Double getTotalScore() {
        // TODO: set weights here
        // return Math.pow(getNlScore(), 2) + getTemplatePopularity() + getNlTemplateAffinity();

        return Math.pow(this.nlScore, 2) * this.complexity;
    }

    public Template getTemplate() {
        return template;
    }

    public void setTemplate(Template template) {
        this.template = template;
    }

    public Double getNlScore() {
        return nlScore;
    }

    public Double getComplexity() {
        return complexity;
    }

    public void setComplexity(Double complexity) {
        this.complexity = complexity;
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
