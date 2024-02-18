var express = require('express')
var app = express()

app.get('/demo/', async function (req, res) {

    res.send('Hello from the Node.js app!')
})

app.get('/demo/cpu-light', async function (req, res) {

    const initialTime = new Date().getTime()
    await someIoBlockingOperation(200)
    await someIoBlockingOperation(200)

    console.log(`Request processed in ${new Date().getTime() - initialTime} ms`)
    res.send('Hello World!')
})

app.get('/demo/cpu-intensive', async function (req, res) {

    const initialTime = new Date().getTime()
    await someIoBlockingOperation(200)
    someCpuIntensiveTask(100)
    await someIoBlockingOperation(200)

    console.log(`Request processed in ${new Date().getTime() - initialTime} ms`)
    res.send('Hello World!')
})

app.listen(3000, function () {
    console.log('Example app listening on port 3000!')
})

function someIoBlockingOperation(ms) {
    return new Promise(resolve => setTimeout(resolve, ms))
}

function someCpuIntensiveTask(ms) {

    const now = new Date().getTime()
    while (now + ms > new Date().getTime()) {
        // do nothing
    }
}