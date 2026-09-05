package ru.yandex.practicum.mymarket.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.List;

@Component
public class DataInitializer implements ApplicationRunner {

    private final ItemRepository itemRepository;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    public DataInitializer(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (seedEnabled && itemRepository.count() == 0) {
            itemRepository.saveAll(catalog());
        }
    }

    private List<Item> catalog() {
        return List.of(
                new Item("Мяч футбольный", "Кожаный мяч стандартного размера для игры на газоне и в зале.",
                        "images/ball.svg", 1490),
                new Item("Конструктор «Строитель»", "Набор из 250 деталей для сборки домов, башен и машин.",
                        "images/constructor.svg", 2990),
                new Item("Кукла «Алиса»", "Модная кукла с набором одежды и аксессуаров.",
                        "images/doll.svg", 1890),
                new Item("Плюшевый медведь", "Мягкий мишка ростом 40 см из гипоаллергенного материала.",
                        "images/bear.svg", 1250),
                new Item("Настольная игра «Монополия»", "Экономическая игра для всей семьи от 8 лет.",
                        "images/game.svg", 2490),
                new Item("Детские кубики", "Набор из 30 ярких деревянных кубиков для малышей.",
                        "images/cubes.svg", 690),
                new Item("Робот-трансформер", "Робот с превращением в машину, управляется жестами.",
                        "images/robot.svg", 3490),
                new Item("Мозаика «Радуга»", "Мозаика из 200 цветных элементов для развития моторики.",
                        "images/mosaic.svg", 790),
                new Item("Машинка-самосвал", "Инерционная машинка с поднимающимся кузовом.",
                        "images/car.svg", 1190),
                new Item("Пазл «Замок»", "Пазл из 1000 деталей с изображением старинного замка.",
                        "images/puzzle.svg", 1390),
                new Item("Юла «Волчок»", "Классическая юла со светящимися узорами.",
                        "images/spinning-top.svg", 390),
                new Item("Железная дорога", "Электрическая железная дорога с поездом и станциями.",
                        "images/train.svg", 4990)
        );
    }
}