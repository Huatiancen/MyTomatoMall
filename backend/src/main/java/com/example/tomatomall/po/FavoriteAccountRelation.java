package com.example.tomatomall.po;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "favorite_account_relation")
public class FavoriteAccountRelation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "account_id", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Account account;

    @ManyToOne
    @JoinColumn(name = "favorite_id", referencedColumnName = "favorite_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Favorite favorite;

    public FavoriteAccountRelation(Account account, Favorite favorite) {
        this.account = account;
        this.favorite = favorite;
    }

}
