import {createRouter, createWebHashHistory} from "vue-router"

const router = createRouter({
    history: createWebHashHistory(),
    routes: [{
        path: '/',
        redirect: '/login',
    }, {
        path: '/login',
        component: () => import('../views/user/Login.vue'),
        meta: {title: '用户登录'}
    }, {
        path: '/register',
        component: () => import('../views/user/Register.vue'),
        meta: {title: '用户注册'}
    }, {
        path: '/dashboard',
        name: 'Dashboard',
        component: () => import('../views/user/Dashboard.vue'),
        meta: {title: '个人中心'}
    }, {
        path: '/home',
        name: 'Home',
        component: () => import('../views/Home.vue'),
        meta: {title: '网页主页'}
    }, {
        path: '/cart',
        name: 'Cart',
        component: () => import('../views/user/Cart.vue'),
        meta: {title: '我的购物车'}
    },{
        path: '/favourites',
        name: 'Favourites',
        component: () => import('../views/user/Favourites.vue'),
        meta: {title: '我的收藏'}
    }, {
        path: '/checkout',
        name: 'Checkout',
        component: () => import('../views/user/Checkout.vue'),
        meta: {title: '发起订单'}
    }, {
        path: '/payment',
        name: 'Order',
        component: () => import('../views/user/Payment.vue'),
        meta: {title: '支付订单'}
    }, {
        path: '/orders',
        name: 'Orders',
        component: () => import('../views/user/Orders.vue'),
        meta: {title: '订单管理'}
    }, {
        path: '/displayBoard',
        name: 'DisplayBoard',
        component: () => import('../views/books/DisplayBoard.vue'),
        meta: {title: '书籍列表'}
    },  {
        path: '/bookDetail/:id',
        name: 'BookDetail',
        component: () => import('../views/books/BookDetail.vue'),
        meta: {title: '书籍详情'}
    },  {
        path: '/addProduct',
        name: 'AddProduct',
        component: () => import('../views/books/AddProduct.vue'),
        meta: {title: '添加书籍'}
    },{
        path: '/404',
        name: '404',
        component: () => import('../views/NotFound.vue'),
        meta: {title: '404'}
    }, {
        path: '/:catchAll(.*)',
        redirect: '/404'
    }, // 商店列表页
    ]
})

router.beforeEach((to, _, next) => {
    const token: string | null = sessionStorage.getItem('token');
    const role: string | null = sessionStorage.getItem('role')

    if (to.meta.title) {
        document.title = to.meta.title
    }

    if (token) {
        if (to.meta.permission) {
            if (to.meta.permission.includes(role!)) {
                next()
            } else {
                next('/404')
            }
        } else {
            next()
        }
    } else {
        if (to.path === '/login') {
            next();
        } else if (to.path === '/register') {
            next()
        } else {
            next('/login')
        }
    }
})

export {router}
