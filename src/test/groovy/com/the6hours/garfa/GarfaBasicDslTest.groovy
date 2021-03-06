package com.the6hours.garfa

import com.googlecode.objectify.Objectify
import com.the6hours.spockappengine.WithGae
import spock.lang.Specification
import com.googlecode.objectify.ObjectifyFactory
import com.the6hours.garfa.testmodels.CarModel
import com.googlecode.objectify.NotFoundException
import com.the6hours.garfa.testmodels.Car
import com.googlecode.objectify.Key

/**
 * 
 * @author Igor Artamonov (http://igorartamonov.com)
 * @since 10.10.11
 */
@WithGae
class GarfaBasicDslTest extends Specification {

    def setup() {
        ObjectifyFactory ofy = new ObjectifyFactory()
        Garfa garfa = new Garfa(ofy)
        [CarModel, Car].each {
             ofy.register(it)
             garfa.register(it)
        }
    }

    def "Null key on a new entity"() {
        setup:
        CarModel m = new CarModel()
        Car car = new Car()
        when:
        def act = m.key
        then:
        act == null
        when:
        act = car.key
        then:
        act == null
    }

    def "Correct key on entity with id"() {
        setup:
        CarModel m = new CarModel(id: 1)
        when:
        def act = m.key
        then:
        act instanceof Key
        act.id == 1
        act.name == null
        act.kind == 'CarModel'
    }

    def "Save car"() {
        setup:
            CarModel car = new CarModel(
                    vendor: 'Vaz',
                    model: '2101'
            )
        when:
            car.save(flush: true)
        then:
            car.id != null
    }

    def "Call beforeXXX on save car"() {
        setup:
            CarModel.__forTesting.beforeSave = 0
            CarModel.__forTesting.beforeInsert = 0
            CarModel car = new CarModel(
                    vendor: 'Vaz',
                    model: '2101'
            )
        when:
            car.save(flush: true)
        then:
            car.id != null
            CarModel.__forTesting.beforeSave == 1
            CarModel.__forTesting.beforeInsert == 1
    }

    def "Get car"() {
        setup:
            CarModel car = new CarModel(
                    vendor: 'Vaz',
                    model: '2101'
            )
            car.save(flush: true)
        when:
            CarModel car2 = CarModel.get(car.id)
        then:
            car2 != null
            car2.id != null
            car2.vendor == 'Vaz'
            car2.model == '2101'
        when:
            CarModel car3 = CarModel.get(car.id + 15123, [safe: true])
        then:
            thrown(NotFoundException)
    }

    def "Get cars list"() {
        setup:
            CarModel car = new CarModel(
                    vendor: 'Vaz',
                    model: '2101'
            )
            car.save(flush: true)
            CarModel car2 = new CarModel(
                    vendor: 'Vaz',
                    model: '2102'
            )
            car2.save(flush: true)
            CarModel car3 = new CarModel(
                    vendor: 'Vaz',
                    model: '2105'
            )
            car3.save(flush: true)
        when:
            List<CarModel> cars = CarModel.get([car.key, car2.key, car3.key])
        then:
            cars != null
            cars.size() == 3
            cars*.model == ['2101', '2102', '2105']
    }

    def "Load car"() {
        setup:
            CarModel car = new CarModel(
                    vendor: 'Vaz',
                    model: '2101'
            )
            car.save(flush: true)
        when:
            CarModel car2 = CarModel.load(car.id)
        then:
            car2 != null
            car2.vendor == 'Vaz'
            car2.model == '2101'
        when:
            CarModel car3 = CarModel.load(car.id + 101234)
        then:
            car3 == null
    }

    def "Load cars list by key"() {
        setup:
            CarModel car = new CarModel(
                    vendor: 'Vaz',
                    model: '2101'
            )
            car.save(flush: true)
            CarModel car2 = new CarModel(
                    vendor: 'Vaz',
                    model: '2102'
            )
            car2.save(flush: true)
            CarModel car3 = new CarModel(
                    vendor: 'Vaz',
                    model: '2105'
            )
            car3.save(flush: true)
            car2.delete(flush: true)
        when:
            def cars = CarModel.load([car.key, car2.key, car3.key])
        then:
            cars != null
            cars.size() == 3
            cars*.model == ['2101', null, '2105']
    }

    def "Load cars list by ids"() {
        setup:
        CarModel model1 = new CarModel(model: 'Ford')
        model1.save(flush: true)
        CarModel model2 = new CarModel(model: 'Dodge')
        model2.save(flush: true)
        CarModel model3 = new CarModel(model: 'Jeep')
        model3.save(flush: true)
        when:
        def act = CarModel.get([model1.id, model3.id])
        then:
        act instanceof List
        act.size() == 2
        act*.model == ['Ford', 'Jeep']
    }


    def "Update car"() {
        setup:
            CarModel car = new CarModel(
                    vendor: 'Vaz',
                    model: '2101'
            )
            car.save(flush: true)
        when:
            CarModel car2 = CarModel.get(car.id)
            car2.update { CarModel curr ->
                curr.model = '2109'
            }
            CarModel car3 = CarModel.get(car.id)
        then:
            car3 != null
            car3.model == '2109'
    }

    def "Update car call beforeXXX"() {
        setup:
            CarModel.__forTesting.beforeSave = 0
            CarModel.__forTesting.beforeInsert = 0
            CarModel.__forTesting.beforeUpdate = 0
            CarModel car = new CarModel(
                    vendor: 'Vaz',
                    model: '2101'
            )
            car.save(flush: true)
        when:
            CarModel car2 = CarModel.get(car.id)
            car2.update { CarModel curr ->
                curr.model = '2109'
            }
            CarModel car3 = CarModel.get(car.id)
        then:
            car3 != null
            car3.model == '2109'
            CarModel.__forTesting.beforeSave == 2 //on save and on update
            CarModel.__forTesting.beforeInsert == 1
            CarModel.__forTesting.beforeUpdate == 1
    }

    def "Update by id"() {
        setup:
        CarModel car = new CarModel(
                vendor: 'Vaz',
                model: '2101'
        )
        car.save(flush: true)
        when:
        CarModel.update (car.id) {
            model = '2108'
        }
        CarModel act = CarModel.get(car.id)
        then:
        act.model == '2108'
    }

    def "Error during update"() {
        setup:
        CarModel car = new CarModel(
                vendor: 'Vaz',
                model: '2101'
        )
        car.save(flush: true)
        when:
        def act = car.update (car.id) {
            model = '2108'
            throw new RuntimeException('test')
        }
        then:
        act == null
        RuntimeException e = thrown(RuntimeException)
        e.message == 'test'
        when:
        CarModel curr = CarModel.get(car.id)
        then:
        curr.model == '2101'
    }

    def "Find first"() {
        setup:
            CarModel car = new CarModel(
                    vendor: 'Vaz',
                    model: '2101'
            )
            car.save(flush: true)
            CarModel car2 = new CarModel(
                    vendor: 'Vaz',
                    model: '2109'
            )
            car2.save(flush: true)
            CarModel car3 = new CarModel(
                    vendor: 'Ford',
                    model: 'Mustang'
            )
            car3.save(flush: true)
        when:
            CarModel found = CarModel.findFirst { filter('vendor =', 'Vaz') }
        then:
            found != null
            found.id == car.id
    }

    def "Find all"() {
        setup:
            CarModel car = new CarModel(
                    vendor: 'Vaz',
                    model: '2101'
            )
            car.save(flush: true)
            CarModel car2 = new CarModel(
                    vendor: 'Vaz',
                    model: '2109'
            )
            car2.save(flush: true)
            CarModel car3 = new CarModel(
                    vendor: 'Ford',
                    model: 'Mustang'
            )
            car3.save(flush: true)
        when:
            List found = CarModel.findAll { filter('vendor =', 'Vaz') }
        then:
            found != null
            found.size() == 2
            found.collect { it.id }.sort() == [car.id, car2.id].sort()
        when:
            found = CarModel.findAll { filter('vendor =', 'Ford') }
        then:
            found != null
            found.size() == 1
            found[0].id == car3.id
    }

    def "Create key for child model"() {
        setup:
            CarModel model = new CarModel(
                    vendor: 'Vaz',
                    model: '2101'
            )
            model.save(flush: true)
            Car car1 = new Car(
                    model: model.key,
                    price: 100
            )
            car1.save(flush: true)
            Car car2 = new Car(
                    model: model.key,
                    price: 120
            )
            car2.save(flush: true)
        when:
            Key key1 = car1.key
            Key modelKey = model.key
        then:
            key1.kind == 'Car'
            key1.parent == modelKey
            modelKey.parent == null
    }

    def "Load children"() {
        setup:
            CarModel model = new CarModel(
                    vendor: 'Vaz',
                    model: '2101'
            )
            model.save(flush: true)
            Car car1 = new Car(
                    model: model.key,
                    price: 100
            )
            car1.save(flush: true)
            Car car2 = new Car(
                    model: model.key,
                    price: 120
            )
            car2.save(flush: true)
        when:
            Car child = model.loadChild(Car, car1.id)
        then:
            child != null
            child.id == car1.id
        when:
            child = model.loadChild(Car, car2.id)
        then:
            child != null
            child.id == car2.id
        when:
            long invalidId = car1.id + car2.id * 31
            child = model.loadChild(Car, invalidId)
        then:
            child == null
    }

    def "Throw exception on empty ID for SAFE get"() {
        when:
        Car.get(null, [safe: true])
        then:
        IllegalArgumentException e = thrown(IllegalArgumentException)
        e.message == 'ID cannot be null for Safe Get'
    }

    def "Run withObjectify"() {
        when:
        def act = Car.withObjectify {
            return delegate
        }
        then:
        act != null
        act instanceof Objectify
    }
}
