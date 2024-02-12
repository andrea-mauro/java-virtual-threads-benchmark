var express = require('express')
var app = express()

app.get('/demo', function (req, res) {
    setTimeout(() => {
        res.send('Hello World!')
    }, 400)
})

app.listen(3000, function () {
    console.log('Example app listening on port 3000!')
})