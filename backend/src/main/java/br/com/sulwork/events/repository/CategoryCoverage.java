package br.com.sulwork.events.repository;

/** The totals of a category, summed by the database. Items split into mandatory and optional. */
public interface CategoryCoverage {

	Long getCategoryId();

	String getName();

	String getUnit();

	Integer getGoalQuantity();

	Long getTotalItems();

	Long getMandatoryRequired();

	Long getOptionalRequired();

	Long getMandatoryContributed();

	Long getOptionalContributed();

}
