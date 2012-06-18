package com.the6hours.groovify.testmodels

import javax.persistence.Id

/**
 * 
 * @author Igor Artamonov (http://igorartamonov.com)
 * @since 10.10.11
 */
class CarModel {

    @Id
    Long id

    String vendor
    String model
    int year

}