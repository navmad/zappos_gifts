package com.navmad.zappos.model;

public class Product {
	int id;
	String name;
	String colorId;
	double price;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double d) {
		this.price = d;
	}

	public String getColorId() {
		return colorId;
	}

	public void setColorId(String color) {
		this.colorId = color;
	}

	public String toString() {
		return "Id : " + getId() + " Price : " + getPrice() + " Color ID : "
				+ getColorId() + " Product Name : " + getName();

	}
}
